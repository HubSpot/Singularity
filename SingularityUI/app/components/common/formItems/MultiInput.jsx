import React, {Component, PropTypes} from 'react';
import FormField from './FormField';
import classNames from 'classnames';

class MultiInput extends Component {

  static propTypes = {
    className: PropTypes.string,
    value: PropTypes.arrayOf(React.PropTypes.string).isRequired,
    onChange: PropTypes.func.isRequired, // Function of signature (newValue) => ()
    placeholder: PropTypes.string,
    errorIndices: PropTypes.arrayOf(PropTypes.number),
    id: PropTypes.string.isRequired,
    doFeedback: PropTypes.bool,
    feedback: PropTypes.oneOf(['ERROR', 'WARN', 'SUCCESS'])
  };

  change(key, value) {
    const valueClone = this.props.value.slice();
    if (key === -1) {
      valueClone.push(value);
    } else {
      valueClone[key] = value;
    }
    this.props.onChange(_.without(valueClone, ''));
  }

  feedbackType(key, value) {
    if ((!this.props.feedback && _.isEmpty(this.props.errorIndices) && !this.props.doFeedback) || (key && !value)) {
      return null;
    }
    if (this.props.feedback) {
      return this.props.feedback;
    }
    if (_.isEmpty(this.props.errorIndices) || this.props.errorIndices.indexOf(key) === -1) {
      return 'SUCCESS';
    }
    return 'ERROR';
  }

  formGroupClassNames(key, value) {
    const feedbackType = this.feedbackType(key, value);
    return classNames(
      {
        'has-success': feedbackType === 'SUCCESS',
        'has-error': feedbackType === 'ERROR',
        'has-warning': feedbackType === 'WARN',
        'has-feedback': feedbackType
      });
  }

  iconClassNames(key, value) {
    const feedbackType = this.feedbackType(key, value);
    return classNames(
      'glyphicon',
      'form-control-feedback',
      {
        'glyphicon-ok': feedbackType === 'SUCCESS',
        'glyphicon-warning-sign': feedbackType === 'WARN',
        'glyphicon-remove': feedbackType === 'ERROR'
      }
    );
  }

  render() {
    const valueClone = this.props.value.slice();
    if (!valueClone.length || _.last(valueClone)) {
      valueClone.push('');
    }
    return (
      <div id={this.props.id} className={this.props.className}>
        {
          valueClone.map((value, key) => {
            return (
              <div className={this.formGroupClassNames(key, value)} key={key} >
                <FormField
                  prop = {{
                    value: value,
                    updateFn: event => this.change(key, event.target.value),
                    type: 'text',
                    placeholder: this.props.placeholder
                  }}
                />
                {this.feedbackType(key, value) && <span className={this.iconClassNames(key, value)} />}
              </div>
            );
          })
        }
      </div>
    );
  }
}

export default MultiInput;
