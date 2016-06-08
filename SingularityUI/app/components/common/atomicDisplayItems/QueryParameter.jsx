import React from 'react';
import classNames from 'classnames';
import IconButton from './IconButton';

let QueryParameter = React.createClass({

    getClassName() {
        return classNames({
            "list-group-item": true,
            "disabled": this.props.prop.cantClear
        });
    },

    render() {
        return <li className={classNames({
            "list-group-item": true,
            "disabled": this.props.prop.cantClear
        })}><b>{this.props.prop.paramName}:</b> {this.props.prop.paramValue}{this.props.prop.cantClear ? <IconButton prop={{
                ariaLabel: 'Remove this parameter',
                iconClass: 'remove',
                className: ['remove-query-param', 'pull-right'],
                btnClass: 'default',
                btn: false,
                onClick: this.props.prop.clearFn
            }} /> : undefined}</li>;
    }
});
export default QueryParameter;

