import React, { Component, PropTypes } from 'react';
import { Row, Col, FormControl } from 'react-bootstrap';
import Utils from '../../../utils';

const renderTextMapInput = (currentValue, onChange, placeholder) => {
  return (
    <FormControl
      type="text"
      value={currentValue || ''}
      placeholder={placeholder}
      onChange={(event) => onChange(event.target.value)}
    />
  );
};

class MapInput extends Component {

  static propTypes = {
    className: PropTypes.string,
    value: PropTypes.arrayOf(React.PropTypes.shape({
      key: PropTypes.any,
      value: PropTypes.any
    })).isRequired,
    renderKeyField: PropTypes.func, // Function of signature (currentValue, onChange, placeholder) => Node
    renderValueField: PropTypes.func, // Function of signature (currentValue, onChange, placeholder) => Node
    keyHeader: PropTypes.string.isRequired,
    valueHeader: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired, // Function of signature (newValue) => ()
    keyPlaceholder: PropTypes.string,
    valuePlaceholder: PropTypes.string,
    errorIndices: PropTypes.arrayOf(PropTypes.number),
    id: PropTypes.string.isRequired,
    doFeedback: PropTypes.bool,
    feedback: PropTypes.oneOf(['ERROR', 'WARN', 'SUCCESS'])
  };

  static defaultProps = {
    renderKeyField: renderTextMapInput,
    renderValueField: renderTextMapInput
  };

  filterOutEmpties(newMapInputValue) {
    return _.filter(newMapInputValue, (valueAtIndex) => _.isObject(valueAtIndex) && (valueAtIndex.key || valueAtIndex.value));
  }

  makeOnChangeFunc(index, isKey) {
    return (newFieldValue) => {
      const newMapInputValue = Utils.deepClone(this.props.value);
      if (isKey) {
        if (_.isObject(newMapInputValue[index])) {
          newMapInputValue[index].key = newFieldValue;
        } else {
          newMapInputValue[index] = { key: newFieldValue };
        }
      } else {
        if (_.isObject(newMapInputValue[index])) {
          newMapInputValue[index].value = newFieldValue;
        } else {
          newMapInputValue[index] = { value: newFieldValue };
        }
      }
      return this.props.onChange(this.filterOutEmpties(newMapInputValue));
    };
  }

  renderKeyField(index) {
    return this.props.renderKeyField(_.isObject(this.props.value[index]) && this.props.value[index].key, this.makeOnChangeFunc(index, true));
  }

  renderValueField(index) {
    return this.props.renderValueField(_.isObject(this.props.value[index]) && this.props.value[index].value, this.makeOnChangeFunc(index, false));
  }

  render() {
    const valueClone = this.props.value.slice();
    if (!valueClone.length || _.last(valueClone)) {
      valueClone.push({});
    }
    return (
      <div id={this.props.id} className={this.props.className}>
        <Row>
          <Col md={6}>
            {this.props.keyHeader}
          </Col>
          <Col md={6}>
            {this.props.valueHeader}
          </Col>
        </Row>
        {
          valueClone.map((value, index) => {
            return (
              <Row key={index}>
                <Col md={6}>
                  {this.renderKeyField(index)}
                </Col>
                <Col md={6}>
                  {this.renderValueField(index)}
                </Col>
              </Row>
            );
          })
        }
      </div>
    );
  }
}

export default MapInput;
