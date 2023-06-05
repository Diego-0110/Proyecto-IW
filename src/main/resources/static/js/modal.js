function toggleModalForm(elementId){
    let formCont = document.getElementById(elementId);
    if(formCont.classList.contains('show')){
        formCont.classList.remove('show');
    }
    else {
        formCont.classList.add('show');
        // Se resetea el formulario contenido:
        let form = document.querySelector('#' + elementId + '>.modal-form');
        if (form.tagName == 'form') {
            form.reset();
        }
    }
}