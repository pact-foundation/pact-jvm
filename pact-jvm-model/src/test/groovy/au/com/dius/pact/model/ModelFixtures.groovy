package au.com.dius.pact.model

import au.com.dius.pact.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.model.matchingrules.TypeMatcher

@Singleton
class ModelFixtures {

  static request = new Request('GET', '/', PactReaderKt.queryStringToMap('q=p&q=p2&r=s'),
    [testreqheader: ['testreqheadervalue']], OptionalBody.body('{"test":true}'.bytes))

  static response = new Response(200, [testreqheader: ['testreqheaderval']],
    OptionalBody.body('{"responsetest":true}'.bytes))

  static requestMatchers = {
    def rules = new MatchingRulesImpl()
    rules.addCategory('body').addRule('$.test', new TypeMatcher())
    rules
  }

  static requestWithMatchers = new Request('GET', '/',
    PactReaderKt.queryStringToMap('q=p&q=p2&r=s'),
    [testreqheader: ['testreqheadervalue']], OptionalBody.body('{"test":true}'.bytes),
    requestMatchers())

  static responseMatchers = {
    def rules = new MatchingRulesImpl()
    rules.addCategory('body').addRule('$.responsetest', new TypeMatcher())
    rules
  }

  static responseWithMatchers = new Response(200, [testreqheader: ['testreqheaderval']],
    OptionalBody.body('{"responsetest":true}'.bytes), responseMatchers())

  static requestNoBody = new Request('GET', '/', PactReaderKt.queryStringToMap('q=p&q=p2&r=s'),
    [testreqheader: ['testreqheadervalue']])

  static requestDecodedQuery = new Request('GET', '/', [datetime: ['2011-12-03T10:15:30+01:00'],
    description: ['hello world!']], [testreqheader: ['testreqheadervalue']],
    OptionalBody.body('{"test":true}'.bytes))

  static responseNoBody = new Response(200, [testreqheader: ['testreqheaderval']])

  static requestLowerCaseMethod = new Request('get', '/',
    PactReaderKt.queryStringToMap('q=p&q=p2&r=s'),
    [testreqheader: ['testreqheadervalue']], OptionalBody.body('{"test":true}'.bytes))
}
