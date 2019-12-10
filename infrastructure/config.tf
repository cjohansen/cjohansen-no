terraform {
  backend "s3" {
    bucket = "cjohansen-infra"
    key = "cjohansen.no/terraform.tfstate"
    region = "eu-north-1"
  }
}

provider "aws" {
  version = "~> 2.0"
  region = "eu-north-1"
}

provider "aws" {
  version = "~> 2.0"
  region = "us-east-1"
  alias = "us-east-1"
}

provider "template" {
  version = "~> 1.0"
}
