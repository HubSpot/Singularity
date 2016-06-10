import React from 'react';
import ReactDOM from 'react-dom';
import View from './view';
import TaskFileBrowser from '../components/taskDetail/TaskFileBrowser';

class FileBrowserSubview extends View {

    events() {
        return {'click [data-directory-path]':  'navigate'};
    }

    constructor(args) {
      super(args);
      this.scrollWhenReady = args.scrollWhenReady;
      this.slaveOffline = args.slaveOffline;
    }

    initialize() {
        this.listenTo(this.collection, 'sync',  this.render);
        this.listenTo(this.collection, 'error', this.catchAjaxError);
        this.listenTo(this.model, 'sync', this.render);
        this.task = this.model;

        return this.scrollAfterRender = Backbone.history.fragment.indexOf('/files') !== -1;
    }

    render() {
        // Ensure we have enough space to scroll
        let offset = this.$el.offset().top;

        let breadcrumbs = utils.pathToBreadcrumbs(this.collection.currentDirectory);

        ReactDOM.render(
            <TaskFileBrowser
                synced = {this.collection.synced && this.task.synced}
                files = {_.pluck(this.collection.models, 'attributes')}
                collection = {this.collection}
                path = {this.collection.path}
                breadcrumbs = {breadcrumbs}
                task = {this.task}
            />
          ,this.el)

        let scroll = () => $(window).scrollTop(this.$el.offset().top - 20);
        if (this.scrollAfterRender) {
            this.scrollAfterRender = false;

            scroll();
            setTimeout(scroll, 100);
        }

        return this.$('.actions-column a[title]').tooltip();
    }

    catchAjaxError() {
        app.caughtError();
        return this.render();
    }
}

export default FileBrowserSubview;
