DIST_ID:=$(shell cd infrastructure && terraform output distribution_id)
BUCKET:=$(shell cd infrastructure && terraform output bucket_name)

target: src/cjohansen_no/* resources/tech/* resources/fermentations/*
	clojure -A:dev:build

clean:
	rm -fr target

deploy: target
	cd target && aws s3 sync . s3://$(BUCKET) --cache-control max-age=31536000,public,immutable --exclude "*" --metadata-directive REPLACE --include "css/*" --include "fonts/*"
	cd target && aws s3 sync . s3://$(BUCKET) --delete --exclude "css/*" --exclude "fonts/*"
	aws cloudfront create-invalidation --distribution-id $(DIST_ID) --paths /index.html /

.PHONY: clean deploy
