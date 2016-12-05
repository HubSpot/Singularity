import React from 'react';
import { Link } from 'react-router';
import classNames from 'classnames';
import ColorDropdown from './ColorDropdown';
import SearchDropdown from './SearchDropdown';
import TasksDropdown from './TasksDropdown';

import { connect } from 'react-redux';
import { switchViewMode, scrollAllToTop, scrollAllToBottom } from '../../actions/log';

class Header extends React.Component {
  renderBreadcrumbs() {
    return this.props.path.split('/').map(function (subpath, i) {
      if (subpath === '$TASK_ID') {
        return <li key={i}><span className="label label-info">Task ID</span></li>;
      } else {
        return <li key={i}>{subpath}</li>;
      }
    });
  }

  renderViewButtons() {
    if (this.props.multipleTasks) {
      return (<div className="btn-group" role="group" title="Select View Type">
        <button type="button" className={classNames({btn: true, 'btn-sm': true, 'btn-default': true, 'no-margin': true, active: this.props.viewMode === 'unified'})} onClick={() => { this.props.switchViewMode('unified'); }}>Unified</button>
        <button type="button" className={classNames({btn: true, 'btn-sm': true, 'btn-default': true, 'no-margin': true, active: this.props.viewMode === 'split'})} onClick={() => { this.props.switchViewMode('split'); }}>Split</button>
      </div>);
    }
  }

  renderAnchorButtons() {
    if (this.props.taskGroupCount > 1) {
      return (<span>
        <a className="btn btn-default btn-sm tail-bottom-button" onClick={this.props.scrollAllToBottom} title="Scroll All to Bottom">
          <span className="glyphicon glyphicon-chevron-down"></span>
        </a>
        <a className="btn btn-default btn-sm tail-top-button" onClick={this.props.scrollAllToTop} title="Scroll All to Top">
          <span className="glyphicon glyphicon-chevron-up"></span>
        </a>
      </span>);
    }
  }

  render() {
    return (
      <div className="tail-header">
        <div className="row">
          <div className="col-md-3">
            <ul className="breadcrumb breadcrumb-request">
              <li>
                Request&nbsp;
                <Link to={`request/${this.props.requestId}`}>
                  {this.props.requestId}
                </Link>
              </li>
            </ul>
          </div>
          <div className="col-md-6">
            <ul className="breadcrumb">
              {this.renderBreadcrumbs()}
            </ul>
          </div>
          <div className="col-md-3 hidden-xs tail-buttons">
            <SearchDropdown />
            {this.props.compressedLogsView ? null : <TasksDropdown />}
            <ColorDropdown />
            {this.renderViewButtons()}
            {this.renderAnchorButtons()}
          </div>
        </div>
      </div>
    );
  }
}

Header.propTypes = {
  requestId: React.PropTypes.string,
  path: React.PropTypes.string.isRequired,
  multipleTasks: React.PropTypes.bool.isRequired,
  viewMode: React.PropTypes.string.isRequired,

  switchViewMode: React.PropTypes.func.isRequired,
  scrollAllToBottom: React.PropTypes.func.isRequired,
  scrollAllToTop: React.PropTypes.func.isRequired,
  compressedLogsView: React.PropTypes.bool.isRequired,
};

function mapStateToProps(state) {
  return {
    taskGroupCount: state.taskGroups.length,
    multipleTasks: (state.taskGroups.length > 1) || ((state.taskGroups.length > 0) && (state.taskGroups[0].taskIds.length > 1)),
    path: state.path,
    viewMode: state.viewMode,
    requestId: state.activeRequest.requestId,
    compressedLogsView: state.logType == 'COMPRESSED'
  };
}

const mapDispatchToProps = { switchViewMode, scrollAllToBottom, scrollAllToTop };

export default connect(mapStateToProps, mapDispatchToProps)(Header);
