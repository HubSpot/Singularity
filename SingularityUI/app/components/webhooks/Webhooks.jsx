import React from 'react';
import ReactDOM from 'react-dom';
import Utils from '../../utils';
import OldTable from '../common/OldTable';
import PlainText from '../common/atomicDisplayItems/PlainText';
import TimeStamp from '../common/atomicDisplayItems/TimeStamp';
import Link from '../common/atomicDisplayItems/Link';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';
import NewWebhookForm from './NewWebhookForm';
import vex from 'vex';
import { connect } from 'react-redux';

let Webhooks = React.createClass({

    defaultRowsPerPage: 10,

    rowsPerPageChoices: [10, 20],

    webhookTypes: ['REQUEST', 'DEPLOY', 'TASK'],

    sortBy(field, sortDirectionAscending) {
        this.props.api.webhooks.data.sortBy(field, sortDirectionAscending);
        return this.forceUpdate();
    },

    webhookColumns() {
        let { sortBy } = this; // JS is annoying
        return [{
            data: 'URL',
            sortable: true,
            doSort: sortDirectionAscending => sortBy('uri', sortDirectionAscending)
        }, {
            data: 'Type',
            sortable: true,
            doSort: sortDirectionAscending => sortBy('type', sortDirectionAscending)
        }, {
            data: 'Timestamp',
            className: 'hidden-xs',
            sortable: true,
            doSort: sortDirectionAscending => sortBy('timestamp', sortDirectionAscending)
        }, {
            data: 'User',
            className: 'hidden-xs',
            sortable: true,
            doSort: sortDirectionAscending => sortBy('user', sortDirectionAscending)
        }, {
            data: 'Queue Size',
            sortable: true,
            doSort: sortDirectionAscending => sortBy('queueSize', sortDirectionAscending)
        }, {
            className: 'hidden-xs'
        }];
    },

    deleteWebhook(webhook) {
        return $.ajax({
            url: `${ config.apiRoot }/webhooks/?webhookId=${ webhook.attributes.webhook.id }`,
            type: "DELETE",
            success: () => this.props.collections.webhooks.fetch().done(() => this.forceUpdate())
        });
    },

    promptDeleteWebhook(webhook) {
        let deleteWebhook = webhook => this.deleteWebhook(webhook);
        return vex.dialog.confirm({
            message: "<div class='delete-webhook' />", // This is not react
            afterOpen: () => {
                return ReactDOM.render(<div><pre>({webhook.attributes.webhook.type}) {webhook.attributes.webhook.uri}</pre><p>Are you sure you want to delete this webhook?</p></div>, $(".delete-webhook").get(0));
            },
            callback: confirmed => {
                if (!confirmed) {
                    return;
                }
                return deleteWebhook(webhook);
            }
        });
    },

    newWebhook(uri, type) {
        let data = {
            uri,
            type
        };
        if (app.user.attributes.authenticated) {
            data.user = app.user.attributes.user.id;
        }
        return $.ajax({
            url: `${ config.apiRoot }/webhooks`,
            type: "POST",
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: () => this.props.collections.webhooks.fetch().done(() => this.forceUpdate())
        });
    },

    promptNewWebhook() {
        let newWebhook = (uri, type) => this.newWebhook(uri, type);
        return vex.dialog.open({
            message: "<div class='new-webhook' />",
            afterOpen: () => {
                this.validateInput = input => {
                    try {
                        new URL(input);
                        return true;
                    } catch (err) {
                        return false;
                    }
                };
                return this.renderedForm = ReactDOM.render(React.createElement(NewWebhookForm, {
                    ["getErrors"]: () => this.errors,
                    "webhookTypes": this.webhookTypes,
                    ["setType"]: selected => this.type = selected,
                    ["setUri"]: uri => this.uri = uri }), $(".new-webhook").get(0));
            },
            beforeClose: () => {
                if (!this.data) {
                    return true;
                }
                this.errors = [];
                let uriValidated = this.validateInput(this.uri);
                if (!this.type) {
                    this.errors.push('Please select a type');
                }
                if (!uriValidated) {
                    this.errors.push('Invalid URL entered');
                }
                if (!uriValidated || !this.type) {
                    this.renderedForm.forceUpdate();
                }
                if (!uriValidated) {
                    return false;
                }
                if (!this.type) {
                    return false;
                }
                this.type = '';
                this.uri = '';
                return true;
            },
            callback: data => {
                this.data = data;
                if (!this.type || !data || !this.validateInput(this.uri)) {
                    return;
                }
                let { type } = this;
                return newWebhook(this.uri, type);
            }
        });
    },

    getWebhookTableData() {
        let data = [];
        this.props.collections.webhooks.map(webhook => data.push({
            dataId: webhook.id,
            dataCollection: 'webhooks',
            data: [{
                component: PlainText,
                prop: {
                    text: webhook.uri
                }
            }, {
                component: PlainText,
                prop: {
                    text: Utils.humanizeText(webhook.type)
                }
            }, {
                component: TimeStamp,
                className: 'hidden-xs',
                prop: {
                    timestamp: webhook.timestamp,
                    display: 'absoluteTimestamp'
                }
            }, {
                component: PlainText,
                className: 'hidden-xs',
                prop: {
                    text: webhook.user || 'N/A'
                }
            }, {
                component: PlainText,
                prop: {
                    text: <b>{webhook.queueSize}</b>
                }
            }, {
                component: Link,
                className: 'hidden-xs actions-column',
                prop: {
                    text: <Glyphicon iconClass='trash' />,
                    onClickFn: () => this.promptDeleteWebhook(webhook),
                    title: 'Delete',
                    altText: "Delete this webhook",
                    overlayTrigger: true,
                    overlayTriggerPlacement: 'top',
                    overlayToolTipContent: 'Delete This Webhook',
                    overlayId: `deleteWebhook${ webhook.id }`
                }
            }]
        }));
        return data;
    },

    render() {
        return <div><div className='row'><div className='col-md-10'><span className='h1'>Webhooks</span></div><div className='col-md-2 button-container'><button className='btn btn-success' alt="Create a new webhook" title="newWebhook" onClick={this.promptNewWebhook}> New Webhook </button></div></div><OldTable defaultRowsPerPage={this.defaultRowsPerPage} rowsPerPageChoices={this.rowsPerPageChoices} tableClassOpts="table-striped" columnHeads={this.webhookColumns()} tableRows={this.getWebhookTableData()} emptyTableMessage='No Webhooks' dataCollection='webhooks' /></div>;
    }
});

function mapStateToProps(state) {
    return {
        collections: {
            webhooks: state.api.webhooks.data
        }
    };
}

export default connect(mapStateToProps)(Webhooks);

