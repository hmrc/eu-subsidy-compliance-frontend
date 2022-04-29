function disableButtonOnClick(id) {
    document
        .getElementById(id)
        .addEventListener('click', (e) => {
            e.stopPropagation()
            e.target.setAttribute('disabled', 'disabled')
            e.target.parentNode.submit()
        })
}