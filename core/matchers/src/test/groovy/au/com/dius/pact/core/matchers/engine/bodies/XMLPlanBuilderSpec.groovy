package au.com.dius.pact.core.matchers.engine.bodies

import au.com.dius.pact.core.matchers.engine.MatchingConfiguration
import au.com.dius.pact.core.matchers.engine.PlanMatchingContext
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import com.github.difflib.DiffUtils
import spock.lang.Specification

import static com.github.difflib.UnifiedDiffUtils.generateUnifiedDiff

class XMLPlanBuilderSpec extends Specification {
  PlanMatchingContext context
  V4Pact pact
  V4Interaction.SynchronousHttp interaction
  MatchingConfiguration config

  def setup() {
    pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    interaction = new V4Interaction.SynchronousHttp('test interaction')
    config = new MatchingConfiguration(false, false, true, false)
    context = new PlanMatchingContext(pact, interaction, config)
  }

  def 'xml plan builder with very simple xml'() {
    given:
    def builder = XMLPlanBuilder.INSTANCE
    def content = '<?xml version="1.0" encoding="UTF-8"?> <blah/>'

    when:
    def node = builder.buildPlan(content.bytes, context)
    def str = new StringBuilder()
    node.prettyForm(str, 0)

    then:
    str.toString() == '''%tee (
    |  %xml:parse (
    |    $.body
    |  ),
    |  :$ (
    |    %if (
    |      %check:exists (
    |        ~>$.blah
    |      ),
    |      :$.blah (
    |        :#text (
    |          %expect:empty (
    |            %to-string (
    |              ~>$.blah['#text']
    |            )
    |          )
    |        ),
    |        %expect:empty (
    |          ~>$.blah
    |        )
    |      ),
    |      %error (
    |        'Was expecting an XML element \\/blah but it was missing'
    |      )
    |    )
    |  )
    |)'''.stripMargin('|')
  }

  def 'xml plan builder with allowed unexpected values'() {
    given:
    def builder = XMLPlanBuilder.INSTANCE
    def content = '<?xml version="1.0" encoding="UTF-8"?> <blah/>'
    config = new MatchingConfiguration(true, false, true, false)
    context = new PlanMatchingContext(pact, interaction, config)

    when:
    def node = builder.buildPlan(content.bytes, context)
    def str = new StringBuilder()
    node.prettyForm(str, 0)

    then:
    str.toString() == '''%tee (
    |  %xml:parse (
    |    $.body
    |  ),
    |  :$ (
    |    %if (
    |      %check:exists (
    |        ~>$.blah
    |      ),
    |      :$.blah (
    |        :#text (
    |          %expect:empty (
    |            %to-string (
    |              ~>$.blah['#text']
    |            )
    |          )
    |        )
    |      ),
    |      %error (
    |        'Was expecting an XML element \\/blah but it was missing'
    |      )
    |    )
    |  )
    |)'''.stripMargin('|')
  }

  def 'xml plan builder with simple xml'() {
    given:
    def builder = XMLPlanBuilder.INSTANCE
    def content = '''<?xml version="1.0" encoding="UTF-8"?>
      <config>
        <name>My Settings</name>
        <sound>
          <property name="volume" value="11" />
          <property name="mixer" value="standard" />
        </sound>
      </config>
    '''

    when:
    def node = builder.buildPlan(content.bytes, context)
    def buffer = new StringBuilder()
    node.prettyForm(buffer, 0)
    def str = buffer.toString()
    println(str)

    def expected = '''%tee (
    |  %xml:parse (
    |    $.body
    |  ),
    |  :$ (
    |    %if (
    |      %check:exists (
    |        ~>$.config
    |      ),
    |      :$.config (
    |        :#text (
    |          %expect:empty (
    |            %to-string (
    |              ~>$.config['#text']
    |            )
    |          )
    |        ),
    |        %expect:only-entries (
    |          ['name', 'sound'],
    |          ~>$.config
    |        ),
    |        %expect:count (
    |          UINT(1),
    |          ~>$.config.name,
    |          %join (
    |            'Expected 1 <name> child element but there were ',
    |            %length (
    |              ~>$.config.name
    |            )
    |          )
    |        ),
    |        %if (
    |          %check:exists (
    |            ~>$.config.name[0]
    |          ),
    |          :$.config.name[0] (
    |            :#text (
    |              %match:equality (
    |                'My Settings',
    |                %to-string (
    |                  ~>$.config.name[0]['#text']
    |                ),
    |                NULL
    |              )
    |            ),
    |            %expect:empty (
    |              ~>$.config.name[0]
    |            )
    |          ),
    |          %error (
    |            'Was expecting an XML element \\/config\\/name\\/0 but it was missing'
    |          )
    |        ),
    |        %expect:count (
    |          UINT(1),
    |          ~>$.config.sound,
    |          %join (
    |            'Expected 1 <sound> child element but there were ',
    |            %length (
    |              ~>$.config.sound
    |            )
    |          )
    |        ),
    |        %if (
    |          %check:exists (
    |            ~>$.config.sound[0]
    |          ),
    |          :$.config.sound[0] (
    |            :#text (
    |              %expect:empty (
    |                %to-string (
    |                  ~>$.config.sound[0]['#text']
    |                )
    |              )
    |            ),
    |            %expect:only-entries (
    |              ['property'],
    |              ~>$.config.sound[0]
    |            ),
    |            %expect:count (
    |              UINT(2),
    |              ~>$.config.sound[0].property,
    |              %join (
    |                'Expected 2 <property> child elements but there were ',
    |                %length (
    |                  ~>$.config.sound[0].property
    |                )
    |              )
    |            ),
    |            %if (
    |              %check:exists (
    |                ~>$.config.sound[0].property[0]
    |              ),
    |              :$.config.sound[0].property[0] (
    |                :attributes (
    |                  :$.config.sound[0].property[0]['@name'] (
    |                    #{"@name='volume'"},
    |                    %if (
    |                      %check:exists (
    |                        ~>$.config.sound[0].property[0]['@name']
    |                      ),
    |                      %match:equality (
    |                        'volume',
    |                        %xml:value (
    |                          ~>$.config.sound[0].property[0]['@name']
    |                        ),
    |                        NULL
    |                      )
    |                    )
    |                  ),
    |                  :$.config.sound[0].property[0]['@value'] (
    |                    #{"@value='11'"},
    |                    %if (
    |                      %check:exists (
    |                        ~>$.config.sound[0].property[0]['@value']
    |                      ),
    |                      %match:equality (
    |                        '11',
    |                        %xml:value (
    |                          ~>$.config.sound[0].property[0]['@value']
    |                        ),
    |                        NULL
    |                      )
    |                    )
    |                  ),
    |                  %expect:entries (
    |                    ['name', 'value'],
    |                    %xml:attributes (
    |                      ~>$.config.sound[0].property[0]
    |                    ),
    |                    %join (
    |                      'The following expected attributes were missing: ',
    |                      %join-with (
    |                        ', ',
    |                        ** (
    |                          %apply ()
    |                        )
    |                      )
    |                    )
    |                  ),
    |                  %expect:only-entries (
    |                    ['name', 'value'],
    |                    %xml:attributes (
    |                      ~>$.config.sound[0].property[0]
    |                    )
    |                  )
    |                ),
    |                :#text (
    |                  %expect:empty (
    |                    %to-string (
    |                      ~>$.config.sound[0].property[0]['#text']
    |                    )
    |                  )
    |                ),
    |                %expect:empty (
    |                  ~>$.config.sound[0].property[0]
    |                )
    |              ),
    |              %error (
    |                'Was expecting an XML element \\/config\\/sound\\/0\\/property\\/0 but it was missing'
    |              )
    |            ),
    |            %if (
    |              %check:exists (
    |                ~>$.config.sound[0].property[1]
    |              ),
    |              :$.config.sound[0].property[1] (
    |                :attributes (
    |                  :$.config.sound[0].property[1]['@name'] (
    |                    #{"@name='mixer'"},
    |                    %if (
    |                      %check:exists (
    |                        ~>$.config.sound[0].property[1]['@name']
    |                      ),
    |                      %match:equality (
    |                        'mixer',
    |                        %xml:value (
    |                          ~>$.config.sound[0].property[1]['@name']
    |                        ),
    |                        NULL
    |                      )
    |                    )
    |                  ),
    |                  :$.config.sound[0].property[1]['@value'] (
    |                    #{"@value='standard'"},
    |                    %if (
    |                      %check:exists (
    |                        ~>$.config.sound[0].property[1]['@value']
    |                      ),
    |                      %match:equality (
    |                        'standard',
    |                        %xml:value (
    |                          ~>$.config.sound[0].property[1]['@value']
    |                        ),
    |                        NULL
    |                      )
    |                    )
    |                  ),
    |                  %expect:entries (
    |                    ['name', 'value'],
    |                    %xml:attributes (
    |                      ~>$.config.sound[0].property[1]
    |                    ),
    |                    %join (
    |                      'The following expected attributes were missing: ',
    |                      %join-with (
    |                        ', ',
    |                        ** (
    |                          %apply ()
    |                        )
    |                      )
    |                    )
    |                  ),
    |                  %expect:only-entries (
    |                    ['name', 'value'],
    |                    %xml:attributes (
    |                      ~>$.config.sound[0].property[1]
    |                    )
    |                  )
    |                ),
    |                :#text (
    |                  %expect:empty (
    |                    %to-string (
    |                      ~>$.config.sound[0].property[1]['#text']
    |                    )
    |                  )
    |                ),
    |                %expect:empty (
    |                  ~>$.config.sound[0].property[1]
    |                )
    |              ),
    |              %error (
    |                'Was expecting an XML element \\/config\\/sound\\/0\\/property\\/1 but it was missing'
    |              )
    |            )
    |          ),
    |          %error (
    |            'Was expecting an XML element \\/config\\/sound\\/0 but it was missing'
    |          )
    |        )
    |      ),
    |      %error (
    |        'Was expecting an XML element \\/config but it was missing'
    |      )
    |    )
    |  )
    |)'''.stripMargin('|')
    def patch = DiffUtils.diff(str, expected, null)
    def diff = generateUnifiedDiff('', '', str.split('\n') as List<String>, patch, 0).join('\n')

    then:
    diff == ''
  }
}
