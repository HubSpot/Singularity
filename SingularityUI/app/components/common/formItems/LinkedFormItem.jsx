import React from 'react';
import classNames from 'classnames';

let LinkedFormItem = React.createClass({

    render() {
        let FormItem1Class = this.props.prop.formItem1.component;
        let FormItem2Class = this.props.prop.formItem2.component;
        return <div id={this.props.id} className={classNames(this.props.prop.customClass, 'text-center')}><FormItem1Class id={this.props.prop.formItem1.id} prop={this.props.prop.formItem1.prop} />{this.props.prop.separator}<FormItem2Class id={this.props.prop.formItem2.id} prop={this.props.prop.formItem2.prop} /></div>;
    }
});

export default LinkedFormItem;

