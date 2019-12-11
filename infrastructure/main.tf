locals {
  bucket_name = "cjohansen.no"
  app_name = "cjohansen.no"
  app_id = "cjohansen-id"
  domain_name = "cjohansen.no"
  hosted_zone = "cjohansen.no."
}

resource "aws_s3_bucket" "bucket" {
  bucket = "${local.bucket_name}"
  acl = "private"
}

resource "aws_cloudfront_origin_access_identity" "identity" {
  comment = "Origin access identity for ${local.app_name}"
}

data "aws_iam_policy_document" "s3_policy" {
  statement {
    actions = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.bucket.arn}/*"]

    principals {
      type = "AWS"
      identifiers = ["${aws_cloudfront_origin_access_identity.identity.iam_arn}"]
    }
  }

  statement {
    actions = ["s3:ListBucket"]
    resources = ["${aws_s3_bucket.bucket.arn}"]

    principals {
      type = "AWS"
      identifiers = ["${aws_cloudfront_origin_access_identity.identity.iam_arn}"]
    }
  }
}

resource "aws_s3_bucket_policy" "bucket_policy" {
  bucket = "${aws_s3_bucket.bucket.id}"
  policy = "${data.aws_iam_policy_document.s3_policy.json}"
}

resource "aws_acm_certificate" "cert" {
  provider = "aws.us-east-1"
  domain_name = "${local.domain_name}"
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

data "aws_route53_zone" "zone" {
  name = "${local.hosted_zone}"
}

resource "aws_route53_record" "cert_validation" {
  name = "${aws_acm_certificate.cert.domain_validation_options.0.resource_record_name}"
  type = "${aws_acm_certificate.cert.domain_validation_options.0.resource_record_type}"
  zone_id = "${data.aws_route53_zone.zone.id}"
  records = ["${aws_acm_certificate.cert.domain_validation_options.0.resource_record_value}"]
  ttl = 60
}

resource "aws_acm_certificate_validation" "cert" {
  provider = "aws.us-east-1"
  certificate_arn = "${aws_acm_certificate.cert.arn}"
  validation_record_fqdns = ["${aws_route53_record.cert_validation.fqdn}"]
}

data "aws_iam_policy_document" "lambda" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type = "Service"
      identifiers = [
        "lambda.amazonaws.com",
        "edgelambda.amazonaws.com"
      ]
    }
  }
}

resource "aws_iam_role" "lambda_role" {
  name_prefix = "${local.domain_name}"
  assume_role_policy = "${data.aws_iam_policy_document.lambda.json}"
}

resource "aws_iam_role_policy_attachment" "lambda_exec" {
  role = "${aws_iam_role.lambda_role.name}"
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

data "archive_file" "rewrite" {
  type = "zip"
  output_path = "${path.module}/.zip/rewrite.zip"

  source {
    filename = "lambda.js"
    content = "${file("${path.module}/url-rewrite.js")}"
  }
}

data "archive_file" "headers" {
  type = "zip"
  output_path = "${path.module}/.zip/headers.zip"

  source {
    filename = "lambda.js"
    content = "${file("${path.module}/security-headers.js")}"
  }
}

resource "aws_lambda_function" "url_rewrite" {
  provider = "aws.us-east-1"
  function_name = "${local.app_id}-url-rewrite"
  filename = "${data.archive_file.rewrite.output_path}"
  source_code_hash = "${data.archive_file.rewrite.output_base64sha256}"
  role = "${aws_iam_role.lambda_role.arn}"
  runtime = "nodejs10.x"
  handler = "lambda.handler"
  memory_size = 128
  timeout = 3
  publish = true
}

resource "aws_lambda_function" "security_headers" {
  provider = "aws.us-east-1"
  function_name = "${local.app_id}-security-headers"
  filename = "${data.archive_file.headers.output_path}"
  source_code_hash = "${data.archive_file.headers.output_base64sha256}"
  role = "${aws_iam_role.lambda_role.arn}"
  runtime = "nodejs10.x"
  handler = "lambda.handler"
  memory_size = 128
  timeout = 3
  publish = true
}

locals {
  s3_origin_id = "StaticFilesS3BucketOrigin"
}

resource "aws_cloudfront_distribution" "s3_distribution" {
  origin {
    domain_name = "${aws_s3_bucket.bucket.bucket_regional_domain_name}"
    origin_id = "${local.s3_origin_id}"

    s3_origin_config {
      origin_access_identity = "${aws_cloudfront_origin_access_identity.identity.cloudfront_access_identity_path}"
    }
  }

  enabled = true
  is_ipv6_enabled = true
  comment = "${local.app_name} distribution"
  default_root_object = "index.html"
  aliases = ["${local.domain_name}"]

  default_cache_behavior {
    allowed_methods  = ["GET", "HEAD", "OPTIONS"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "${local.s3_origin_id}"

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }

    viewer_protocol_policy = "redirect-to-https"
    min_ttl = 0
    default_ttl = 3600
    max_ttl = 86400
    compress = true

    lambda_function_association {
      event_type = "viewer-request"
      lambda_arn = "${aws_lambda_function.url_rewrite.qualified_arn}"
      include_body = false
    }

    lambda_function_association {
      event_type = "viewer-response"
      lambda_arn = "${aws_lambda_function.security_headers.qualified_arn}"
      include_body = false
    }
  }

  price_class = "PriceClass_100"

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn = "${aws_acm_certificate.cert.arn}"
    minimum_protocol_version = "TLSv1.1_2016"
    ssl_support_method = "sni-only"
  }

  custom_error_response {
    error_code = "404"
    response_code = "404"
    response_page_path = "/404.html"
  }
}

resource "aws_route53_record" "record" {
  name = "${local.domain_name}"
  zone_id = "${data.aws_route53_zone.zone.zone_id}"
  type = "A"

  alias {
    name = "${aws_cloudfront_distribution.s3_distribution.domain_name}"
    zone_id = "${aws_cloudfront_distribution.s3_distribution.hosted_zone_id}"
    evaluate_target_health = true
  }
}

output "distribution_id" {
  value = "${aws_cloudfront_distribution.s3_distribution.id}"
}

output "distribution_arn" {
  value = "${aws_cloudfront_distribution.s3_distribution.arn}"
}

output "bucket_arn" {
  value = "${aws_s3_bucket.bucket.arn}"
}

output "bucket_name" {
  value = "${local.bucket_name}"
}
