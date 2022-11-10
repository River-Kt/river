#!/bin/bash

awslocal lambda create-function \
  --function-name hello_world \
  --runtime python \
  --zip-file fileb:///docker-entrypoint-initaws.d/hello_lambda.py.zip \
  --handler hello_lambda.hello_handler \
  --role sample
