import React, {PropTypes} from 'react';
import MultiInput from '../MultiInput';
import { FormGroup, ControlLabel } from 'react-bootstrap/lib';

const MultiInputFormGroup = (props) => (
  <FormGroup id={props.id} className={props.required && 'required'}>
    <ControlLabel>{props.label}</ControlLabel>
    <MultiInput
      className={props.id}
      value={props.value || []}
      onChange={props.onChange}
      doFeedback={props.couldHaveFeedback && (props.required || !_.isEmpty(props.value))}
      {...props}
    />
  </FormGroup>
);

MultiInputFormGroup.propTypes = {
  id: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  value: PropTypes.arrayOf(PropTypes.string),
  required: PropTypes.bool,
  couldHaveFeedback: PropTypes.bool,
  errorIndices: PropTypes.arrayOf(PropTypes.number)
};

export default MultiInputFormGroup;
