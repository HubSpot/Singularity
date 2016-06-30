import React from 'react';
import classNames from 'classnames';
import Utils from '../../../utils';

let TaskStateLabel = React.createClass({

    getLabelClass() {
        return Utils.getLabelClassFromTaskState(this.props.prop.taskState);
    },

    getClass() {
        return classNames('label', `label-${ this.getLabelClass() }`, this.props.prop.className);
    },

    render() {
        return <span className={this.getClass()} label={`Task State: ${ Utils.humanizeText(this.props.prop.taskState) }`}>{Utils.humanizeText(this.props.prop.taskState)}</span>;
    }
});

export default TaskStateLabel;
