import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { FetchPriorityFreeze, NewPriorityFreeze } from '../../actions/api/disasters';
import FormModal from '../common/modal/FormModal';


class EditPriorityFreezeModal extends Component {
  static propTypes = {
    user: PropTypes.string,
    freeze: PropTypes.shape({
      minimumPriorityLevel: PropTypes.number,
      killTasks: PropTypes.bool,
      message: PropTypes.string,
      actionId: PropTypes.string,
    }),
    newPriorityFreeze: PropTypes.func.isRequired,
  };

  show() {
    this.refs.editPriorityFreezeModal.show();
  }

  render() {
    return (
      <FormModal
        ref="editPriorityFreezeModal"
        name="Edit Priority Freeze"
        action="Edit Priority Freeze"
        buttonStyle="warning"
        onConfirm={(data) => this.props.newPriorityFreeze(data.minimumPriorityLevel, data.killTasks, data.message)}
        formElements={[
          {
            type: FormModal.INPUT_TYPES.NUMBER,
            name: 'minimumPriorityLevel',
            label: 'Minimum Priority Level',
            isRequired: true,
            defaultValue: this.props.freeze.minimumPriorityLevel,
            max: 1.0,
            min: 0,
            step: 0.1,
          },
          {
            type: FormModal.INPUT_TYPES.BOOLEAN,
            name: 'killTasks',
            label: 'Kill Tasks?',
            defaultValue: this.props.freeze.killTasks,
          },
          {
            type: FormModal.INPUT_TYPES.STRING,
            name: 'message',
            label: 'Message',
            isRequired: false,
            defaultValue: this.props.freeze.message,
          }
        ]}
      />
    );
  }
}

const mapDispatchToProps = (dispatch) => ({
  newPriorityFreeze: (minPriority, killTasks, message) => dispatch(NewPriorityFreeze.trigger(minPriority, killTasks, message)).then(() => {dispatch(FetchPriorityFreeze.trigger([404]));}),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(EditPriorityFreezeModal);
