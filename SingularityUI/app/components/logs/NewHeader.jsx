import React from 'react';
import { Link } from 'react-router';
import classNames from 'classnames';
import ColorDropdown from './ColorDropdown';
import TasksDropdown from './TasksDropdown';

import { connect } from 'react-redux';
import { switchViewMode } from '../../actions/log';
import { jumpAllToTop, jumpAllToBottom } from '../../actions/tailer';

class NewHeader extends React.Component {
  renderBreadcrumbs() {
    if (this.props.paths.length > 1) {
      return (<li>(multiple paths)</li>);
    } else if (this.props.paths.length === 1) {
      return this.props.paths[0].split('/').map((subpath, key) => {
        if (subpath === '$TASK_ID') {
          return <li key={key}><span className="label label-info">Task ID</span></li>;
        }
        return <li key={key}>{subpath}</li>;
      });
    }
  }

  renderViewButtons() {
    if (this.props.taskIdCount > 1) {
      return (<div className="btn-group" role="group" title="Select View Type">
        <button type="button" className={classNames({btn: true, 'btn-sm': true, 'btn-default': true, 'no-margin': true, active: this.props.viewMode === 'unified'})} onClick={() => { this.props.switchViewMode('unified'); }}>Unified</button>
        <button type="button" className={classNames({btn: true, 'btn-sm': true, 'btn-default': true, 'no-margin': true, active: this.props.viewMode === 'split'})} onClick={() => { this.props.switchViewMode('split'); }}>Split</button>
      </div>);
    }
  }

  renderAnchorButtons() {
    return (this.props.tailerGroupCount > 1) && (
      <span>
        <a className="btn btn-default btn-sm tail-bottom-button" onClick={this.props.jumpAllToBottom} title="Scroll All to Bottom">
          <span className="glyphicon glyphicon-chevron-down"></span>
        </a>
        <a className="btn btn-default btn-sm tail-top-button" onClick={this.props.jumpAllToTop} title="Scroll All to Top">
          <span className="glyphicon glyphicon-chevron-up"></span>
        </a>
      </span>
    );
  }

  renderRequestLink() {
    if (this.props.requestIds.length > 1) {
      return (<li>
        Requests&nbsp;
        {this.props.requestIds.map((requestId, key) => (<Link key={key} to={`request/${requestId}`}>{requestId}</Link>))}
      </li>);
    } else if (this.props.requestIds.length === 1) {
      return (<li>
        Request&nbsp;
        <Link to={`request/${this.props.requestIds[0]}`}>
          {this.props.requestIds[0]}
        </Link>
      </li>);
    }
  }

  render() {
    if (!this.props.ready) {
      return (<div>Loading...</div>);
    }

    const tasksDropdown = (this.props.requestIds.length === 1) && (<TasksDropdown />);

    return (
      <div className="tail-header">
        <div className="row">
          <div className="col-md-3">
            <ul className="breadcrumb breadcrumb-request">
              {this.renderRequestLink()}
            </ul>
          </div>
          <div className="col-md-6">
            <ul className="breadcrumb">
              {this.renderBreadcrumbs()}
            </ul>
          </div>
          <div className="col-md-3 hidden-xs tail-buttons">
            { tasksDropdown }
            <ColorDropdown />
            {this.renderViewButtons()}
            {this.renderAnchorButtons()}
          </div>
        </div>
      </div>
    );
  }
}

NewHeader.propTypes = {
  //requestIds: React.PropTypes.string,
  //paths: React.PropTypes.string.isRequired,
  viewMode: React.PropTypes.string.isRequired,
  tailerGroupCount: React.PropTypes.number.isRequired,
  switchViewMode: React.PropTypes.func.isRequired,
  jumpAllToTop: React.PropTypes.func.isRequired,
  jumpAllToBottom: React.PropTypes.func.isRequired,
};

const mapDispatchToProps = { switchViewMode, jumpAllToTop, jumpAllToBottom };

export default connect((state) => ({
  tailerGroupCount: state.tailerView.tailerGroups.length,
  taskIdCount: state.tailerView.taskIds.length,
  requestIds: state.tailerView.requestIds,
  paths: state.tailerView.paths,
  viewMode: state.tailerView.viewMode,
  ready: state.tailerView.ready
}), mapDispatchToProps)(NewHeader);
