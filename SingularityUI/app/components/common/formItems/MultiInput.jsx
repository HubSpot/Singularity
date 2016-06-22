import React from 'react';
import FormField from './FormField';
import classNames from 'classnames';
import Utils from '../../../utils';

let MultiInput = React.createClass({

  propTypes: {
    className: React.PropTypes.string,
    value: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
    onChange: React.PropTypes.func.isRequired // Function of signature (newValue) => ()
  },

  change(key, value) {
    const valueClone = this.props.value.slice();
    if (key === -1) {
      valueClone.push(value);
    } else {
      valueClone[key] = value;
    }
    this.props.onChange(_.without(valueClone, ""));
  },

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
                  required: this.props.required,
                  type: "text",
                  placeholder: this.props.placeholder
                }}
              />
            );
          })
        }
      </div>
    );
  }
});

export default MultiInput;
