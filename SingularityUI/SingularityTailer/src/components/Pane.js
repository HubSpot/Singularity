import React, { PropTypes } from 'react';

const Pane = ({logHeader, logComponent, logFooter}) => {
  return (
      <section className="log-pane">
        <header>
          {logHeader}
        </header>
        {logComponent}
        <footer>
          {logFooter}
        </footer>
      </section>
  );
};

Pane.propTypes = {
  logHeader: PropTypes.node.isRequired,
  logComponent: PropTypes.node.isRequired,
  logFooter: PropTypes.node.isRequired
};

export default Pane;
