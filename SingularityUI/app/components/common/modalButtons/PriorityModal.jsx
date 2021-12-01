import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { PriorityRequest } from '../../../actions/api/requests.es6';

import FormModal from '../modal/FormModal';

class PriorityModal extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    current: PropTypes.number,
    submit: PropTypes.func,
    children: PropTypes.node,
    then: PropTypes.func
  };

  componentDidMount() {
    if (window.location.search.includes('priority=true')) {
      this.show();
    }
  }

  handle(data) {
    const { priority, durationMillis, message } = data;
    this.props.submit(
      {
        priority,
        durationMillis,
        message
      }
    );
  }

  show() {
    this.refs.modal.show();
  }

  render() {
    return (
      <FormModal
        name="Set Request Priority"
        ref="modal"
        action="Set Request Priority"
        onConfirm={(data) => this.handle(data)}
        buttonStyle="primary"
        formElements={[
          {
            name: 'priority',
            min: 0,
            type: FormModal.INPUT_TYPES.NUMBER,
            label: 'Request scheduling priority: (0.0 to 1.0)',
            defaultValue: this.props.current,
            isRequired: true
          },
          {
            name: 'durationMillis',
            type: FormModal.INPUT_TYPES.DURATION,
            label: 'Expiration: (optional)'
          },
          {
            name: 'message',
            type: FormModal.INPUT_TYPES.STRING,
            label: 'Message: (optional)'
          }
        ]}>
        <p>Setting request priority:</p>
        <pre>{this.props.requestId}</pre>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  submit: (data) => dispatch(PriorityRequest.trigger(ownProps.requestId, data)).then((response) => ownProps.then && ownProps.then(response)),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(PriorityModal);
