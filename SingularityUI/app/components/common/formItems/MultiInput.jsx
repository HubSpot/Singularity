import React, {Component, PropTypes} from 'react';
import FormField from './FormField';
import classNames from 'classnames';
import Utils from '../../../utils';

class MultiInput extends Component {

  static propTypes = {
    className: PropTypes.string,
    value: PropTypes.arrayOf(React.PropTypes.string).isRequired,
    onChange: PropTypes.func.isRequired, // Function of signature (newValue) => ()
    placeholder: PropTypes.string
  };

  change(key, value) {
    const valueClone = this.props.value.slice();
    if (key === -1) {
      valueClone.push(value);
    } else {
      valueClone[key] = value;
    }
    this.props.onChange(_.without(valueClone, ""));
  };

  render() {
    const valueClone = this.props.value.slice()
    if (valueClone.length === 0 || _.last(valueClone)) {
      valueClone.push("")
    }
    return (
      <div id={this.props.id} className={this.props.className}>
        {
          valueClone.map((value, key) => {
            return (
              <FormField
                key = {key}
                prop = {{
                  value: value,
                  updateFn: event => this.change(key, event.target.value),
                  /* required: this.props.required, Can't mark this as form required because there will always be an extra empty field */
                  type: "text",
                  placeholder: this.props.placeholder
                }}
              />
            );
          })
        }
      </div>
    );
  };
}

export default MultiInput;
