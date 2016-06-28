import React from 'react';
import moment from 'moment';
import FormField from './FormField';
import Glyphicon from '../atomicDisplayItems/Glyphicon';
import datetimepicker from 'eonasdan-bootstrap-datetimepicker';

let DateEntry = React.createClass({

    componentWillReceiveProps(nextProps) {
        let id = `#${ this.props.id }`;
        //datetimepicker = $(id).data('datetimepicker');
        if (datetimepicker) {
            if (nextProps.prop.value !== this.props.prop.value) {
                if (!nextProps.prop.value) {
                    return datetimepicker.setDate(null);
                }
            }
        }
    },

    initializeDateTimePicker() {
        let id = `#${ this.props.id }`;
        let changeFn = this.props.prop.updateFn;
        return $(() => $(id).datetimepicker({
            sideBySide: true,
            format: window.config.timestampFormat
        }).on('dp.change', changeFn)); // value will be in event.date
    },

    getValue() {
        if (!this.props.prop.value) {
            return;
        }
        let time = moment(this.props.prop.value);
        return time.format(window.config.timestampFormat);
    },

    // MUST pass in UNIQUE id in props.
    // Otherwise the datetime picker will break in ways that aren't even very interesting
    render() {
        return <div className={`input-group date ${ this.props.prop.customClass }`} id={this.props.id}><FormField id={this.props.id} prop={{
                updateFn: this.props.prop.updateFn,
                value: this.getValue(),
                size: this.props.prop.size,
                disabled: this.props.prop.disabled,
                type: this.props.prop.inputType,
                placeholder: this.props.prop.placeholder,
                required: this.props.prop.required,
                customClass: this.props.prop.customClass
            }} /><span className='input-group-addon' onMouseOver={this.initializeDateTimePicker}><Glyphicon iconClass='calendar' /></span></div>;
    }
});

export default DateEntry;

