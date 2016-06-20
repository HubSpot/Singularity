import React from 'react';
import classNames from 'classnames';
import Utils from '../../../utils';

let FormField = React.createClass({

  componentDidMount() {
    if (this.props.prop.generateSelectBox) {
      let generateSelectBox = () => {
        $(`#${this.props.id}`).select2(this.props.prop.selectBoxOptions).on('change', this.props.prop.updateFn);
      }
      generateSelectBox();
      // Delay needed because componentDidMount() is called before the entire document is rendered to HTML
      // There is no lifecycle method that runs as soon as the document is rendered as HTML...
      setTimeout(generateSelectBox, 1);
    }
  },

  render() {
    return <input 
      className={classNames({'form-control': !this.props.noFormControlClass}, this.props.prop.customClass)}
      placeholder={this.props.prop.placeholder}
      type={this.props.prop.inputType}
      id={this.props.id}
      onChange={this.props.prop.updateFn}
      onClick={this.props.prop.onClick}
      value={this.props.prop.value || ""}
      disabled={this.props.prop.disabled}
      min={this.props.prop.min}
      max={this.props.prop.max}
      required={this.props.prop.required}
      checked={this.props.prop.checked || false}
    />;
  }
});

export default FormField;

