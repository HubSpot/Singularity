import React from 'react';
import classNames from 'classnames';

let IconButton = React.createClass({

    propTypes: {
        prop: React.PropTypes.shape({
            ariaLabel: React.PropTypes.string.isRequired,
            className: React.PropTypes.oneOfType([React.PropTypes.string, React.PropTypes.object //Classnames object
            ]),
            btn: React.PropTypes.boolean,
            btnClass: React.PropTypes.string.isRequired,
            iconClass: React.PropTypes.string.isRequired,
            onClick: React.PropTypes.func.isRequired
        }).isRequired
    },

    render() {
        return <button aria-label={this.props.prop.ariaLabel} alt={this.props.prop.ariaLabel} className={classNames(this.props.prop.className, { 'btn': this.props.prop.btn !== false }, `btn-${ this.props.prop.btnClass }`, 'glyphicon', `glyphicon-${ this.props.prop.iconClass }`)} onClick={this.props.prop.onClick} />;
    }
});

export default IconButton;

