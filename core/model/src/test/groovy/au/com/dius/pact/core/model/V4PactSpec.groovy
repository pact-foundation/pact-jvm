package au.com.dius.pact.core.model

import spock.lang.Specification

class V4PactSpec extends Specification {

  def 'test load v4 pact'() {
    given:
    def pactUrl = V4PactSpec.classLoader.getResource('v4-http-pact.json')

    when:
    def pact = DefaultPactReader.INSTANCE.loadPact(pactUrl)

    then:
    pact instanceof V4Pact
    pact.consumer.name == 'test_consumer'
    pact.provider.name == 'test_provider'
    pact.interactions.size() == 1
    pact.interactions[0].uniqueKey() == '001'
    pact.interactions[0] instanceof V4Interaction.SynchronousHttp
    pact.interactions[0].description == 'test interaction with a binary body'
    pact.metadata['pactSpecification']['version'] == '4.0'
  }

//  #[test]
//  fn test_load_v4_message_pact() {
//    let pact_file = fixture_path("v4-message-pact.json");
//    let pact_result = read_pact(&pact_file);
//
//    match pact_result {
//      Ok(pact) => {
//        let mut f = File::open(pact_file).unwrap();
//        let pact_json_from_file : serde_json::Value = serde_json::de::from_reader(&mut f).unwrap();
//        let pact_json = pact.to_json(PactSpecification::V4);
//        expect!(pact_json.get("consumer")).to(be_equal_to(pact_json_from_file.get("consumer")));
//        expect!(pact_json.get("provider")).to(be_equal_to(pact_json_from_file.get("provider")));
//        expect!(pact_json.get("interactions")).to(be_equal_to(pact_json_from_file.get("interactions")));
//
//        expect!(pact.metadata().get("pactSpecification").clone()).to(be_some().value(&btreemap!(
//        "version".to_string() => "4.0".to_string())));
//        let metadata = pact_json.get("metadata").unwrap().as_object().unwrap();
//        let expected_keys : Vec<String> = vec![s!("pactRust"), s!("pactSpecification")];
//        expect!(metadata.keys().cloned().collect::<Vec<String>>()).to(be_equal_to(expected_keys));
//        expect!(metadata.get("pactSpecification").unwrap().to_string()).to(be_equal_to(s!("{\"version\":\"4.0\"}")));
//      },
//      Err(err) => panic!("Failed to load pact from '{:?}' - {}", pact_file, err)
//    }
//  }
//
//  #[test]
//  fn test_load_v4_combined_pact() {
//    let pact_file = fixture_path("v4-combined-pact.json");
//    let pact_result = read_pact(&pact_file);
//
//    match pact_result {
//      Ok(pact) => {
//        let mut f = File::open(pact_file).unwrap();
//        let pact_json_from_file : serde_json::Value = serde_json::de::from_reader(&mut f).unwrap();
//        let pact_json = pact.to_json(PactSpecification::V4);
//        expect!(pact_json.get("consumer")).to(be_equal_to(pact_json_from_file.get("consumer")));
//        expect!(pact_json.get("provider")).to(be_equal_to(pact_json_from_file.get("provider")));
//        expect!(pact_json.get("interactions")).to(be_equal_to(pact_json_from_file.get("interactions")));
//
//        expect!(pact.metadata().get("pactSpecification").clone()).to(be_some().value(&btreemap!(
//        "version".to_string() => "4.0".to_string())));
//        let metadata = pact_json.get("metadata").unwrap().as_object().unwrap();
//        let expected_keys : Vec<String> = vec![s!("pactRust"), s!("pactSpecification")];
//        expect!(metadata.keys().cloned().collect::<Vec<String>>()).to(be_equal_to(expected_keys));
//        expect!(metadata.get("pactSpecification").unwrap().to_string()).to(be_equal_to(s!("{\"version\":\"4.0\"}")));
//      },
//      Err(err) => panic!("Failed to load pact from '{:?}' - {}", pact_file, err)
//    }
//  }

}
