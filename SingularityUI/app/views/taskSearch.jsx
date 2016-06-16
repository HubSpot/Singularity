import React from 'react';
import ReactDOM from 'react-dom';

import View from './view';

import TaskSearch from '../components/taskSearch/TaskSearch';

class TaskSearchView extends View {

    constructor(...args) {
        super(...args);
        this.viewJson = this.viewJson.bind(this);
    }

    events() {
        return _.extend(super.events(),
            {'click [data-action="viewJSON"]': 'viewJson'});
    }

    viewJson(e) {
        let $target = $(e.currentTarget).parents('tr');
        let id = $target.data('id');
        let collectionName = $target.data('collection');

        // Need to reach into subviews to get the necessary data
        let { collection } = this.subviews[collectionName];
        return utils.viewJSON(collection.get(id));
    }

    initialize({requestId, global}, opts) {
        this.requestId = requestId;
        this.global = global;
        this.opts = opts;
    }

    remove() {
        super.remove();
        ReactDOM.unmountComponentAtNode(this.el);
    }

    render() {
      $(this.el).addClass("task-search-root");
      ReactDOM.render(
        <TaskSearch
         initialRequestId = {this.requestId}
         global = {this.global}
         taskSearchViewSuper = {this.super}
        />,
      this.el);
  }
}

export default TaskSearchView;
