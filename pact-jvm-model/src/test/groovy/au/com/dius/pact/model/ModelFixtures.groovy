package au.com.dius.pact.model

@Singleton
class ModelFixtures {

  static request = new Request('GET', '/', PactReader.queryStringToMap('q=p&q=p2&r=s'),
    [testreqheader: 'testreqheadervalue'], OptionalBody.body('{"test":true}'))

  static response = new Response(200, [testreqheader: 'testreqheaderval'], OptionalBody.body('{"responsetest":true}'))

  static requestWithMatchers = new Request('GET', '/', PactReader.queryStringToMap('q=p&q=p2&r=s'),
    [testreqheader: 'testreqheadervalue'], OptionalBody.body('{"test":true}'),
    ['$.body.test': [match: 'type']])

  static responseWithMatchers = new Response(200, [testreqheader: 'testreqheaderval'],
    OptionalBody.body('{"responsetest":true}'), ['$.body.responsetest': [match: 'type']])

  static requestNoBody = new Request('GET', '/', PactReader.queryStringToMap('q=p&q=p2&r=s'),
    [testreqheader: 'testreqheadervalue'])

  static requestDecodedQuery = new Request('GET', '/', [datetime: ['2011-12-03T10:15:30+01:00'],
    description: ['hello world!']], [testreqheader: 'testreqheadervalue'],
    OptionalBody.body('{"test":true}'))

  static responseNoBody = new Response(200, [testreqheader: 'testreqheaderval'])

  static requestLowerCaseMethod = new Request('get', '/', PactReader.queryStringToMap('q=p&q=p2&r=s'),
    [testreqheader: 'testreqheadervalue'], OptionalBody.body('{"test":true}'))
}
