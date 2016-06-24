import React from 'react';
import classNames from 'classnames';

let Glyphicon = React.createClass({
    render() {
        let className = classNames('glyphicon', `glyphicon-${ this.props.iconClass || this.props.prop.iconClass } ${this.props.spin ? 'glyphicon-spin' : ''}`);
        return <span className={className} aria-hidden='true' />;
    }
});

export default Glyphicon;
