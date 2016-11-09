import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import FormModal from '../common/modal/FormModal';
import Utils from '../../utils';
import { UpdateSlavesTableSettings } from '../../actions/ui/slaves';

class CustomizeSlavesTableModal extends Component {
  static propTypes = {
    columns: PropTypes.object.isRequired,
    paginated: PropTypes.bool.isRequired,
    availableAttributes: PropTypes.arrayOf(PropTypes.string).isRequired,
    availableResources: PropTypes.arrayOf(PropTypes.string).isRequired,
    updateSlaveTableSettings: PropTypes.func.isRequired
  };

  show() {
    this.refs.customizeSlavesTableModal.show();
  }

  render() {
    const formElements = [];
    for (var field in Utils.DEFAULT_SLAVES_COLUMNS) {
      formElements.push(
        {
          name: field,
          type: FormModal.INPUT_TYPES.BOOLEAN,
          label: `Default Field: ${field}`,
          defaultValue: field in this.props.columns ? this.props.columns[field] : false
        }
      )
    }
    _.each(this.props.availableResources, (field) => {
      formElements.push(
        {
          name: field,
          type: FormModal.INPUT_TYPES.BOOLEAN,
          label: `Resource: ${field}`,
          defaultValue: field in this.props.columns ? this.props.columns[field] : false
        }
      )
    });
    _.each(this.props.availableAttributes, (field) => {
      formElements.push(
        {
          name: field,
          type: FormModal.INPUT_TYPES.BOOLEAN,
          label: `Attribute: ${field}`,
          defaultValue: field in this.props.columns ? this.props.columns[field] : false
        }
      )
    });
    formElements.push(
      {
        name: 'paginated',
        type: FormModal.INPUT_TYPES.BOOLEAN,
        label: 'paginated',
        defaultValue: this.props.paginated
      }
    );
    return (
      <FormModal
        ref="customizeSlavesTableModal"
        name="Customize Columns"
        action="Update"
        buttonStyle="default"
        onConfirm={(data) => this.props.updateSlaveTableSettings(data)}
        keepCurrentFormState={true}
        formElements={formElements}
      />
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  updateSlaveTableSettings: (data) => dispatch(UpdateSlavesTableSettings(data, data.paginated)),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(CustomizeSlavesTableModal);
