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
    required: PropTypes.bool,
    feedback: PropTypes.oneOf(['SUCCESS', 'ERROR', 'WARN'])
  }

  formGroupClassNames() {
    return classNames(
      'form-group',
      {
        required: this.props.required,
        'has-success': this.props.feedback === 'SUCCESS',
        'has-error': this.props.feedback === 'ERROR',
        'has-warning': this.props.feedback === 'WARN',
        'has-feedback': this.props.feedback
      });
  }

  iconClassNames() {
    return classNames(
      'glyphicon',
      'form-control-feedback',
      {
        'glyphicon-ok': this.props.feedback === 'SUCCESS',
        'glyphicon-warning-sign': this.props.feedback === 'WARN',
        'glyphicon-remove': this.props.feedback === 'ERROR'
      }
    );
  }

  render() {
    return (
      <div className={this.formGroupClassNames()}>
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
        {this.props.feedback && <span className={this.iconClassNames()} />}
      </div>
    );
  }
}

export default TextFormGroup;
