import View from './view';
import React from 'react';
import ReactDOM from 'react-dom';
import AggregateTail from '../components/aggregateTail/AggregateTail';

class AggregateTailView extends View {

    // Single Mode: Backwards compatability mode for the old URL format. Disables all task switching controls.
    constructor(...args) {
      super(...args);
      this.handleViewChange = this.handleViewChange.bind(this);
    }

    initialize({requestId, path, ajaxError, offset, activeTasks, logLines, singleMode, singleModeTaskId}) {
      this.requestId = requestId;
      this.path = path;
      this.ajaxError = ajaxError;
      this.offset = offset;
      this.activeTasks = activeTasks;
      this.logLines = logLines;
      this.singleMode = singleMode;
      this.singleModeTaskId = singleModeTaskId;
      return window.addEventListener('viewChange', this.handleViewChange);
    }

    handleViewChange() {
      let unmounted = ReactDOM.unmountComponentAtNode(this.el);
      if (unmounted) {
        return window.removeEventListener('viewChange', this.handleViewChange);
      }
    }

    render() {
      $(this.el).addClass("tail-root");
      ReactDOM.render(
        <AggregateTail
          requestId={this.requestId}
          path={this.path}
          initialOffset={this.offset}
          ajaxError={this.ajaxError}
          logLines={this.logLines}
          activeTasks={this.activeTasks}
          singleMode={this.singleMode}
          singleModeTaskId={this.singleModeTaskId}
        />,
        this.el);
    }
  }

export default AggregateTailView;
