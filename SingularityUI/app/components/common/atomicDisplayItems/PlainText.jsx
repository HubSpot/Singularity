import React from 'react';

let PlainText = React.createClass({

    propTypes: {
        prop: React.PropTypes.shape({
            text: React.PropTypes.node.isRequired,
            className: React.PropTypes.string
        }).isRequired
    },

    render() {
        return <div className={this.props.prop.className}>{this.props.prop.text}</div>;
    }
});

export default PlainText;

