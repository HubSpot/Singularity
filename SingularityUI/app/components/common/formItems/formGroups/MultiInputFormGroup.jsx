import React, {PropTypes} from 'react';
import MultiInput from '../MultiInput';
import classNames from 'classnames';

const MultiInputFormGroup = (props) => (
  <div className={classNames('form-group', {required: props.required})}>
    <label htmlFor={props.id}>{props.label}</label>
    <MultiInput
      id={props.id}
      className={props.id}
      value={props.value || []}
      onChange={props.onChange}
      required={props.required}
      placeholder={props.placeholder}
      errorIndices={props.errorIndices}
      doFeedback={props.couldHaveFeedback && (props.required || !_.isEmpty(props.value))}
    />
  </div>
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
