import React, { PropTypes } from 'react';
import Section from '../common/Section';
import Column from '../common/table/Column';
import UITable from '../common/table/UITable';
import DisasterButton from './DisasterButton';
import AutomatedActionsButton from './AutomatedActionsButton'
import Utils from '../../utils';

const DISASTER_TYPES = ['EXCESSIVE_TASK_LAG', 'LOST_SLAVES', 'LOST_TASKS', 'USER_INITIATED']

function ManageDisasters (props) {
  var actionButtonClass;
  var automatedActionButtonAction;
  if (props.automatedActionsDisabled) {
    automatedActionButtonAction = "Enable"
    actionButtonClass = "btn btn-primary pull-right"
  } else {
    automatedActionButtonAction = "Disable"
    actionButtonClass = "btn btn-warning pull-right"
  }
  return (
    <Section title="Manage Disasters">
      <div className="row">
        <AutomatedActionsButton 
          user={props.user}
          action={automatedActionButtonAction}
        >
          <button
            className={actionButtonClass}
            alt={automatedActionButtonAction}
            title={automatedActionButtonAction}>
            {automatedActionButtonAction} Automated Actions
          </button>
        </AutomatedActionsButton>
      </div>
      <UITable
        emptyTableMessage="No Disaster Data Found"
        data={props.disasters}
        keyGetter={(disaster) => disaster.type}
        defaultSortBy="type"
        defaultSortDirection={UITable.SortDirection.ASC}
      >
        <Column
          label="Type"
          id="type"
          key="type"
          sortable={true}
          sortData={(cellData, disaster) => disaster.type}
          cellData={(disaster) => Utils.humanizeText(disaster.type)}
        />
        <Column
          label="State"
          id="state"
          key="state"
          cellData={(disaster) => 
            <span className={disaster.active ? 'label label-danger' : 'label label-primary'}>
              {disaster.active ? "Active" : "Inactive"}
            </span>
          }
        />
        <Column
          id="actions-column"
          key="actions-column"
          className="actions-column"
          cellData={(disaster) =>
            <DisasterButton 
              user={props.user}
              action={disaster.active ? "Deactivate" : "Activate"}
              type={disaster.type}
            >
              <button
                className={disaster.active ? "btn btn-primary" : "btn btn-warning"}
                alt={disaster.active ? "Deactivate" : "Activate"}
                title={disaster.active ? "Deactivate" : "Activate"}>
                {disaster.active ? "Deactivate" : "Activate"}
              </button>
            </DisasterButton>
          }
        />
      </UITable>
    </Section>
  );
}

ManageDisasters.propTypes = {
  disasters: PropTypes.arrayOf(PropTypes.shape({
    type: PropTypes.string.isRequired,
    active: PropTypes.bool
  })).isRequired,
  automatedActionsDisabled: PropTypes.bool
};

export default ManageDisasters;