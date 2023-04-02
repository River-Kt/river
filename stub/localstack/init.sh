#!/bin/bash

awslocal lambda create-function \
  --function-name hello_world \
  --runtime python3.9 \
  --zip-file fileb:///etc/localstack/init/ready.d/hello_lambda.py.zip \
  --handler hello_lambda.hello_handler \
  --role arn:aws:iam::000000000000:role/lambda-role
