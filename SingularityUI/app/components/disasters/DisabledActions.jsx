import React, { PropTypes } from 'react';
import Utils from '../../utils';
import Column from '../common/table/Column';
import UITable from '../common/table/UITable';
import Section from '../common/Section';
import DeleteDisabledActionButton from './DeleteDisabledActionButton';
import NewDisabledActionButton from './NewDisabledActionButton';

function DisabledActions (props) {
  return (
    <Section title="Disabled Actions">
      <div className="row">
        <NewDisabledActionButton user={props.user}>
          <button
            className="btn btn-warning pull-right"
            alt="Disable an Action"
            title="newDisabledAction">
            New Disabled Action
          </button>
        </NewDisabledActionButton>
      </div>
      <UITable
        emptyTableMessage="No Actions Are Disabled"
        data={props.disabledActions}
        keyGetter={(disabledAction) => disabledAction.type}
        defaultSortBy="type"
        defaultSortDirection={UITable.SortDirection.ASC}
      >
        <Column
          label="Type"
          id="type"
          key="type"
          sortable={true}
          sortData={(cellData, disabledAction) => disabledAction.type}
          cellData={(disabledAction) => Utils.humanizeText(disabledAction.type)}
        />
        <Column
          label="Message"
          id="message"
          key="message"
          cellData={(disabledAction) => disabledAction.message}
        />
        <Column
          label="User"
          id="user"
          key="user"
          cellData={(disabledAction) => disabledAction.user}
        />
        <Column
          id="actions-column"
          key="actions-column"
          className="actions-column"
          cellData={(disabledAction) => <DeleteDisabledActionButton disabledAction={disabledAction} />}
        />
      </UITable>
    </Section>
  );
}

DisabledActions.propTypes = {
  disabledActions: PropTypes.arrayOf(PropTypes.shape({
    type: PropTypes.string.isRequired,
    message: PropTypes.string,
    user: PropTypes.string
  })).isRequired,
  user: PropTypes.string
};

export default DisabledActions;
