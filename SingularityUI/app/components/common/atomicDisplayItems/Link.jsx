import React from 'react';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

let Link = React.createClass({

    propTypes: {
        prop: React.PropTypes.shape({
            className: React.PropTypes.oneOfType([React.PropTypes.string, React.PropTypes.object //Classnames object
            ]),
            url: React.PropTypes.string,
            onClickFn: React.PropTypes.func,
            title: React.PropTypes.string,
            text: React.PropTypes.node,
            id: React.PropTypes.string,
            overlayId: React.PropTypes.string,
            overlayToolTipContent: React.PropTypes.node,
            overlayTrigger: React.PropTypes.boolean,
            overlayTriggerPlacement: React.PropTypes.oneOf(['top', 'bottom', 'left', 'right'])
        }).isRequired
    },

    getLink() {
        return <a href={this.props.prop.url} title={this.props.prop.title} onClick={this.props.prop.onClickFn} className={this.props.prop.className} id={this.props.id}>{this.props.prop.text}</a>;
    },

    getToolTip() {
        return <ToolTip id={this.props.prop.overlayId}>{this.props.prop.overlayToolTipContent}</ToolTip>;
    },

    render() {
        if (this.props.prop.overlayTrigger) {
            return <OverlayTrigger placement={this.props.prop.overlayTriggerPlacement} overlay={this.getToolTip()}>{this.getLink()}</OverlayTrigger>;
        } else {
            return this.getLink();
        }
    }
});

export default Link;
