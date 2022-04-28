function disableButtonOnClick(id) {
    document
        .getElementById(id)
        .addEventListener('click', (e) => {
            e.stopPropagation()
            e.target.disabled = true;
            e.target.parentNode.submit()
        })
}