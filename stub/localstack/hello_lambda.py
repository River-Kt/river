def hello_handler(event, context):
  return {
    'message': 'hello, world! your message was {}'.format(event['message'])
  }
