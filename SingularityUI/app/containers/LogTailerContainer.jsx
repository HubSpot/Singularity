import React from 'react';
import classNames from 'classnames';
import { TailerProvider, SandboxTailer } from 'singularityui-tailer';
import NewTaskGroupHeader from '../components/logs/NewTaskGroupHeader';
import NewHeader from '../components/logs/NewHeader';
import FileNotFound from '../components/logs/FileNotFound';
import { withRouter, Link } from 'react-router';
import { connect } from 'react-redux';
import ReactDOM from 'react-dom';
import { actions as tailerActions } from 'singularityui-tailer';
import { Glyphicon } from 'react-bootstrap';

import { loadColor, removeTailerGroup, pickTailerGroup, jumpToBottom, jumpToTop } from '../actions/tailer';

const prefixedLineLinkRenderer = (taskId, path) => ({start}) => {
  return (<a
    href={`${ config.appRoot }/task/${taskId}/tail/${path}?offset=${start}`}
    className="offset-link"
  >
    <div className="pre-line">
      <span className="glyphicon glyphicon-link" />
    </div>
  </a>);
}

class LogTailerContainer extends React.PureComponent {
  constructor() {
    super();
    this.state = {
      notFound: {}
    };
  }

  componentWillMount() {
    this.props.loadColor();
    document.addEventListener(tailerActions.SINGULARITY_TAILER_AJAX_ERROR_EVENT, (event) => {
      if (event.detail.response.status == 404 && event.detail.taskId) {
        const notFound = {};
        notFound[event.detail.taskId] = true;
        this.setState({
          notFound: notFound
        });
      }
    });
  }

  render() {
    const renderTailerPane = (tasks, key) => {
      const {taskId, path, offset, tailerId} = tasks[0];

      if (this.state.notFound[taskId]) {
        const fileName = _.last(path.split('/'));
        console.log(fileName)

        return (<section className="log-pane" key={key}>
          <div className="row tail-row tail-row-centered">
              <div className="not-found-message">
                <p>
                  {fileName} does not exist in this directory.
                </p>
                <Link to={`/task/${taskId}`}>
                  <Glyphicon glyph="arrow-left" /> Back to Task Detail Page
                </Link>
              </div>
          </div>
        </section>);
      } else {

        return (<section className="log-pane" key={key}>
          <NewTaskGroupHeader
            taskId={taskId}
            showRequestId={this.props.requestIds.length > 1}
            showCloseAndExpandButtons={this.props.tailerGroups.length > 1}
            onClose={() => this.props.removeTailerGroup(key)}
            onExpand={() => this.props.pickTailerGroup(key)}
            onJumpToTop={() => this.props.jumpToTop(tailerId, taskId, path, this.props.router)}
            onJumpToBottom={() => this.props.jumpToBottom(tailerId, taskId, path)} />
          <SandboxTailer
            goToOffset={parseInt(offset)}
            tailerId={tailerId}
            taskId={taskId}
            path={path.replace('$TASK_ID', taskId)}
            lineLinkRenderer={prefixedLineLinkRenderer(taskId, path)} />
        </section>);
      }
    };

    return (
      <TailerProvider getTailerState={(state) => state.tailer}>
        <div className={classNames(['new-tailer', 'tail-root', this.props.color])}>
          <NewHeader />
          <div className="row tail-row">
            {this.props.tailerGroups.map(renderTailerPane)}
          </div>
        </div>
      </TailerProvider>
    );
  }
}

export default connect((state) => ({
  tailerGroups: state.tailerView.tailerGroups,
  requestIds: state.tailerView.requestIds,
  color: state.tailerView.color
}), {
  loadColor,
  removeTailerGroup,
  pickTailerGroup,
  jumpToBottom,
  jumpToTop,
})(withRouter(LogTailerContainer));
