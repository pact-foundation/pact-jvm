
impl XmlValue {
  /// Returns the value if it is an XML element
  pub fn as_element(&self) -> Option<Element> {
    match self {
      XmlValue::Element(element) => Some(element.clone()),
      _ => None
    }
  }

  /// Returns the value if it is XML text
  pub fn as_text(&self) -> Option<String> {
    match self {
      XmlValue::Text(text) => Some(text.clone()),
      _ => None
    }
  }

  /// Returns the value if it is an XML attribute
  pub fn as_attribute(&self) -> Option<(String, String)> {
    match self {
      XmlValue::Attribute(name, value) => Some((name.clone(), value.clone())),
      _ => None
    }
  }
}


impl From<XmlResult> for XmlValue {
  fn from(value: XmlResult) -> Self {
    match value {
      XmlResult::ElementNode(element) => XmlValue::Element(element),
      XmlResult::TextNode(text) => XmlValue::Text(text),
      XmlResult::Attribute(name, value) => XmlValue::Attribute(name, value)
    }
  }
}

impl Matches<XmlValue> for XmlValue {
  fn matches_with(
    &self,
    actual: XmlValue,
    matcher: &MatchingRule,
    cascaded: bool
  ) -> anyhow::Result<()> {
    self.matches_with(&actual, matcher, cascaded)
  }
}

impl Matches<&XmlValue> for XmlValue {
  fn matches_with(
    &self,
    actual: &XmlValue,
    matcher: &MatchingRule,
    cascaded: bool
  ) -> anyhow::Result<()> {
    match self {
      XmlValue::Element(expected) => if let Some(actual) = actual.as_element() {
        expected.matches_with(&actual, matcher, cascaded)
      } else {
        Err(anyhow!("Was expecting an XML element but got {}", actual))
      }
      XmlValue::Text(expected) => if let Some(actual) = actual.as_text() {
        expected.matches_with(actual, matcher, cascaded)
      } else {
        Err(anyhow!("Was expecting XML text but got {}", actual))
      }
      XmlValue::Attribute(_, expected_value) => if let Some((_, value)) = actual.as_attribute() {
        expected_value.matches_with(value, matcher, cascaded)
      } else {
        Err(anyhow!("Was expecting an XML attribute but got {}", actual))
      }
    }
  }
}

impl Matches<&Element> for Element {
  fn matches_with(
    &self,
    actual: &Element,
    matcher: &MatchingRule,
    cascaded: bool
  ) -> anyhow::Result<()> {
    let result = match matcher {
      MatchingRule::Regex(regex) => {
        match Regex::new(regex) {
          Ok(re) => {
            if re.is_match(actual.name().as_str()) {
              Ok(())
            } else {
              Err(anyhow!("Expected '{}' to match '{}'", actual.name(), regex))
            }
          },
          Err(err) => Err(anyhow!("'{}' is not a valid regular expression - {}", regex, err))
        }
      },
      MatchingRule::Type => if self.name() == actual.name() {
        Ok(())
      } else {
        Err(anyhow!("Expected '{}' to be the same type as '{}'", self.name(), actual.name()))
      },
      MatchingRule::MinType(min) => if !cascaded && actual.children().count() < *min {
        Err(anyhow!("Expected '{}' to have at least {} children", actual.name(), min))
      } else {
        Ok(())
      },
      MatchingRule::MaxType(max) => if !cascaded && actual.children().count() > *max {
        Err(anyhow!("Expected '{}' to have at most {} children", actual.name(), max))
      } else {
        Ok(())
      },
      MatchingRule::MinMaxType(min, max) => {
        let children = actual.children().count();
        if !cascaded && children < *min {
          Err(anyhow!("Expected '{}' to have at least {} children", actual.name(), min))
        } else if !cascaded && children > *max {
          Err(anyhow!("Expected '{}' to have at most {} children", actual.name(), max))
        } else {
          Ok(())
        }
      },
      MatchingRule::Equality => {
        if self.name() == actual.name() {
          Ok(())
        } else {
          Err(anyhow!("Expected '{}' to be equal to '{}'", self.name(), actual.name()))
        }
      },
      MatchingRule::NotEmpty => if actual.children().next().is_some() {
        Err(anyhow!("Expected '{}' to have at least one child", actual.name()))
      } else {
        Ok(())
      },
      _ => Err(anyhow!("Unable to match {:?} using {:?}", self, matcher))
    };
    debug!("Comparing '{:?}' to '{:?}' using {:?} -> {:?}", self, actual, matcher, result);
    result
  }
}

pub(crate) fn name(element: &Element) -> String {
  if let Some(namespace) = element.namespace() {
    format!("{}:{}", namespace, element.name())
  } else {
    element.name()
  }
}
