import React from 'react';

export default class Breadcrumbs extends React.Component {

  renderItems() {
    return this.props.items.map((i, n) => {
      if (i.link) {
        return (
          <li key={n}>{i.label} <a href={i.link}>{i.text}</a></li>
        );
      } else {
        return (
          <li key={n}>{i.label} {i.text}</li>
        );
      }
    });
  }

  render() {
    return (
      <ul className="breadcrumb">
          {this.renderItems()}
      </ul>
    );
  }
}

Breadcrumbs.propTypes = {
  items: React.PropTypes.arrayOf(React.PropTypes.shape({
      label: React.PropTypes.string.isRequired,
      text: React.PropTypes.string.isRequired,
      link: React.PropTypes.string
  })).isRequired
};
