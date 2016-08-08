import React, { PropTypes } from 'react';
import { Row, Col, FormControl } from 'react-bootstrap';
import Utils from '../../../utils';
import classNames from 'classnames';

const renderTextMapInput = (currentValue, onChange, placeholder) => (
  <FormControl
    type="text"
    value={currentValue || ''}
    placeholder={placeholder}
    onChange={(event) => onChange(event.target.value)}
  />
);

const MapInput = (props) => {
  const filterOutEmpties = (newMapInputValue) => (
    _.filter(newMapInputValue, (valueAtIndex) => _.isObject(valueAtIndex) && (!_.isEmpty(valueAtIndex.key) || !_.isEmpty(valueAtIndex.value)))
  );

  const hasNoFeedback = (index, value) => {
    if (_.isEmpty(value) || (_.isEmpty(value.key) && _.isEmpty(value.value))) {
      return true;
    }
    return !props.feedback &&
      _.isEmpty(props.errorIndices) &&
      !props.doFeedback &&
      (value ? _.isEmpty(value.value) || value.key : true);
  };

  const feedbackType = (index, value) => {
    if (hasNoFeedback(index, value)) {
      return null;
    }
    if (value && !_.isEmpty(value.value) && !value.key) {
      return 'ERROR';
    }
    if (props.feedback) {
      return props.feedback;
    }
    if (_.isEmpty(props.errorIndices) || props.errorIndices.indexOf(index) === -1) {
      return 'SUCCESS';
    }
    return 'ERROR';
  };

  const formGroupClassNames = (index, value) => {
    const feedback = feedbackType(index, value);
    return classNames({
      'has-success': feedback === 'SUCCESS',
      'has-error': feedback === 'ERROR',
      'has-warning': feedback === 'WARN',
      'has-feedback': feedback
    });
  };

  const iconClassNames = (index, value) => {
    const feedback = feedbackType(index, value);
    return classNames(
      'glyphicon',
      'form-control-feedback',
      {
        'glyphicon-ok': feedback === 'SUCCESS',
        'glyphicon-warning-sign': feedback === 'WARN',
        'glyphicon-remove': feedback === 'ERROR'
      }
    );
  };

  const makeOnChangeFunc = (index, isKey) => (newFieldValue) => {
    const newMapInputValue = Utils.deepClone(props.value);
    if (isKey) {
      if (_.isObject(newMapInputValue[index])) {
        newMapInputValue[index].key = newFieldValue;
      } else {
        newMapInputValue[index] = { key: newFieldValue, value: props.valueDefault };
      }
    } else {
      if (_.isObject(newMapInputValue[index])) {
        newMapInputValue[index].value = newFieldValue;
      } else {
        newMapInputValue[index] = { value: newFieldValue };
      }
    }
    return props.onChange(filterOutEmpties(newMapInputValue));
  };

  const renderKeyField = (index) => (
    props.renderKeyField(_.isObject(props.value[index]) && props.value[index].key, makeOnChangeFunc(index, true))
  );

  const renderValueField = (index) => (
    props.renderValueField(_.isObject(props.value[index]) && props.value[index].value, makeOnChangeFunc(index, false))
  );

  const valueClone = props.value.slice();
  if (!valueClone.length || _.last(valueClone)) {
    if (props.valueDefault) {
      valueClone.push({value: props.valueDefault});
    } else {
      valueClone.push({});
    }
  }
  return (
    <div id={props.id} className={props.className}>
      <Row>
        <Col md={6}>
          {props.keyHeader}
        </Col>
        <Col md={6}>
          {props.valueHeader}
        </Col>
      </Row>
      {
        valueClone.map((value, index) => (
          <div className={formGroupClassNames(index, value)} key={index}>
            <Row key={index}>
              <Col md={6}>
                {renderKeyField(index)}
                {feedbackType(index, value) && <span className={iconClassNames(index, value)} />}
              </Col>
              <Col md={6}>
                {renderValueField(index)}
                {feedbackType(index, value) && <span className={iconClassNames(index, value)} />}
              </Col>
            </Row>
          </div>
        ))
      }
    </div>
  );
};

MapInput.propTypes = {
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
  valueDefault: PropTypes.any,
  feedback: PropTypes.oneOf(['ERROR', 'WARN', 'SUCCESS'])
};

MapInput.defaultProps = {
  renderKeyField: renderTextMapInput,
  renderValueField: renderTextMapInput
};

export default MapInput;
