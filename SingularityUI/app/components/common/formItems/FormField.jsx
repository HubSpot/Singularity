import React from 'react';
import classNames from 'classnames';
import Utils from '../../../utils';

let FormField = React.createClass({

    render() {
        return <input 
            className={classNames('form-control', this.props.prop.customClass)}
            placeholder={this.props.prop.placeholder}
            type={this.props.prop.inputType}
            id={this.props.id}
            onChange={this.props.prop.updateFn}
            onClick={this.props.prop.onClick}
            value={this.props.prop.value}
            disabled={this.props.prop.disabled}
            min={this.props.prop.min}
            max={this.props.prop.max}
            required={this.props.prop.required}
        />;
    }
});

export default FormField;

