import React from 'react';

export const getClickComponent = (component, doFirst = (() => Promise.resolve())) => (
  React.Children.map(component.props.children, child => (
    React.cloneElement(child, {
      onClick: () => doFirst().then(() => component.refs.modal.getWrappedInstance().show())
    })
  ))
);
