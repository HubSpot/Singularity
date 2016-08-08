import React, { PropTypes } from 'react';
import MapInput from '../MapInput';
import { FormGroup, ControlLabel } from 'react-bootstrap/lib';

const MapInputFormGroup = (props) => (
  <FormGroup id={props.id} className={props.required && 'required'}>
    <ControlLabel>{props.label}</ControlLabel>
    <MapInput
      className={props.id}
      value={props.value || []}
      {...props}
    />
  </FormGroup>
);

MapInputFormGroup.propTypes = {
  id: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  renderKeyField: PropTypes.func, // Function of signature (currentValue, onChange) => Node
  renderValueField: PropTypes.func, // Function of signature (currentValue, onChange) => Node
  keyPlaceholder: PropTypes.string,
  valuePlaceholder: PropTypes.string,
  keyHeader: PropTypes.string.isRequired,
  valueHeader: PropTypes.string.isRequired,
  value: PropTypes.arrayOf(React.PropTypes.shape({
    key: PropTypes.any,
    value: PropTypes.any
  })).isRequired,
  required: PropTypes.bool
};

export default MapInputFormGroup;
