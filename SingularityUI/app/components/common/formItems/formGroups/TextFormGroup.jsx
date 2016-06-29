import React, {Component, PropTypes} from 'react';
import FormField from '../FormField';
import classNames from 'classnames';

class TextFormGroup extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    placeholder: PropTypes.string,
    value: PropTypes.string,
    required: PropTypes.bool
  }

  render() {
    return (
      <div className={classNames('form-group', {required: this.props.required})}>
        <label htmlFor={this.props.id}>{this.props.label}</label>
        <FormField
          id={this.props.id}
          className={this.props.id}
          prop = {{
            updateFn: this.props.onChange,
            inputType: 'text',
            value: this.props.value,
            required: this.props.required,
            placeholder: this.props.placeholder
          }}
        />
      </div>
    );
  }
}

export default TextFormGroup;
