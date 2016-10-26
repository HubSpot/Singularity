import Select from 'react-select';
import React, { PropTypes } from 'react';

import 'react-select/scss/default';

// Wrapper for multi-selects in which pressing comma, space, or enter,
// or blurring, causes the option to be autocompleted and added.

const MultiSelect = (props) => {
  const valueToLabelMap = [];
  for (const option of props.options) {
    valueToLabelMap[option.value] = option.label;
  }

  const addNewOption = (valueToAdd) => {
    let cleansedValueToAdd = valueToAdd;
    if (props.splits) {
      for (const split of props.splits) {
        cleansedValueToAdd = cleansedValueToAdd.split(split).join('');
      }
    }
    if (!cleansedValueToAdd || props.value.indexOf(cleansedValueToAdd) !== -1) {
      return false;
    }
    if (_.find(props.value, option => option.value === cleansedValueToAdd)) {
      return false;
    }
    const chosenOption = _.find(props.options, option => option.value === cleansedValueToAdd);
    if (!chosenOption) {
      return false;
    }
    const newValue = props.value.slice() || [];
    newValue.push(chosenOption);
    props.onChange(newValue);
    return true;
  };

  const checkInputChange = (value) => {
    if (props.splits && props.splits.indexOf(value.slice(-1)) !== -1 && addNewOption(value)) { // Side effect!
      return '';
    }
    return value;
  };

  const getValueAsObj = (value) => {
    if (props.isValueString) {
      return value.map(valueArrayContent => ({value: valueArrayContent, label: valueToLabelMap[valueArrayContent]}));
    }
    return value;
  };

  const onChange = (newValue) => {
    if (props.isValueString && newValue) {
      return props.onChange(newValue.map(valueArrayContent => valueArrayContent.value));
    }
    if (!newValue) {
      return props.onChange([]);
    }
    return props.onChange(newValue);
  };

  return (
    <Select
      id={ props.id }
      onChange={ onChange }
      onInputChange={ value => checkInputChange(value) }
      value={ getValueAsObj(props.value) }
      options={ props.options }
      onBlurResetsInput={ false }
      multi={ true }
      onBlur={ event => addNewOption(event.target.value) }
      placeholder={ props.placeholder || '' }
      allowCreate={ props.allowCreate || !props.options }
    />
  );
};

MultiSelect.propTypes = {
  splits: PropTypes.arrayOf(PropTypes.string),
  value: PropTypes.arrayOf(PropTypes.oneOfType([
    PropTypes.shape({
      label: PropTypes.string,
      value: PropTypes.string
    }),
    PropTypes.string
  ])).isRequired,
  options: PropTypes.arrayOf(PropTypes.shape({
    label: PropTypes.string,
    value: PropTypes.string
  })).isRequired,
  onChange: PropTypes.func.isRequired,
  isValueString: PropTypes.bool,
  placeholder: PropTypes.string,
  allowCreate: PropTypes.bool,
  id: PropTypes.string.isRequired
};

export default MultiSelect;
