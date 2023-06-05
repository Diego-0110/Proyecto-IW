function updateElementsJoin(display, text, addition){
    document.getElementById("acts-aft-join").style.display = display;
    document.getElementById("acts-aft-join").style.flexDirection = "column";
    document.getElementById("text-join").innerHTML = text;
    let takenVacs = Number(document.getElementById("occupied").innerText);
    takenVacs += addition;
    document.getElementById("occupied").innerText = takenVacs;
    let capacity = Number(document.getElementById("capacity").innerText);
    document.getElementById("pbar-vacs-progress").style.width = ((takenVacs / capacity) * 200) + "px";
}

function toggleJoin(elem, eventId){
    if(elem.checked){
        let joined = true;
        go(`/event/${eventId}/userEvent`, "POST", {joined}, false)
            .then(d => {console.log(d);
                updateElementsJoin('flex', 'Salir', 1);})
            .catch(e => {//console.log(e)
                alert("Something went wrong.");
                elem.checked = false});
    }
    else {
        let joined = false;
        go(`/event/${eventId}/userEvent`, "POST", {joined}, false)
            .then(d => {console.log(d);
                updateElementsJoin('none', 'Unirse', -1);})
            .catch(e => {//console.log(e)
                alert("Something went wrong.");
                elem.checked = true});
    }
}

function updateElementsFav(icon, heart, num, addition){
    icon.innerText = heart;
    if (num != null) { // Search view case
        num.innerText = Number(num.innerText) + addition;
    }
}

function toggleFav(elem, eventId){
    let numf = document.getElementById("fav-n");
    if(elem.innerText == "❤️"){
        let fav = false;
        go(`/event/${eventId}/userEvent`, "POST", {fav}, false)
            .then(d => {console.log(d);
                updateElementsFav(elem, '🤍', numf, -1);})
            .catch(e => //console.log(e)
                alert("Something went wrong."));
    }
    else{
        let fav = true;
        go(`/event/${eventId}/userEvent`, "POST", {fav}, false)
            .then(d => {console.log(d);
                updateElementsFav(elem, '❤️', numf, 1);})
            .catch(e => //console.log(e)
                alert("Something went wrong."));
    }
}

function reportUser(userTarget, description = ""){
    description = document.getElementById('report-desc').value;
    let event = document.getElementById('report-event').value;
    let cause = document.getElementById('report-cause').value;
    go(`/user/${userTarget}/report`, "POST", {description, event, cause}, false)
        .then(d => {console.log(d);
            toggleModalForm('report-form-cont');})
        .catch(e => {console.log(e)
            alert("Something went wrong.");
            });
}

function rateEvent(eventId){
    let rating = document.querySelector('#rate-event-form-cont input[name="rating"]:checked').value;
    let description = document.querySelector('#rate-event-form-cont textarea[name="description"]').value;
    go(`/event/${eventId}/rate`, "POST", {rating, description}, false)
        .then(d => {console.log(d);
            toggleModalForm('rate-event-form-cont');})
        .catch(e => {console.log(e)
            alert("Something went wrong.");
            });
}

function rateUserEvent(userId, eventId){
    let rating = document.querySelector('#user-' + userId + '-rating input[name="rating"]:checked').value;
    let description = document.querySelector('#user-' + userId + '-rating textarea[name="description"]').value;
    go(`/event/${eventId}/rateUser/${userId}`, "POST", {rating, description}, false)
        .then(d => {console.log(d);
            // toggleModalForm('rate-event-form-cont');
            alert("Rating sended successfully.");
            })
        .catch(e => {console.log(e)
            alert("Something went wrong.");
            });
}

function toggleUserRating(userId, button){
    let activeCont = document.querySelector('#rate-users-form-cont .user-rating-cont.d-block');
    if (activeCont) {
        activeCont.classList.remove('d-block');
        activeCont.classList.add('d-none');
        let activeButton = document.querySelector('#rate-users-form-cont .b-user-sel-rating.btn-primary');
        if(activeButton){
            activeButton.classList.remove('btn-primary');
            activeButton.classList.add('btn-outline-primary');
        }
    }
    let newActiveCont = document.querySelector('#user-' + userId + '-rating');
    newActiveCont.classList.remove('d-none');
    newActiveCont.classList.add('d-block');
    button.classList.remove('btn-outline-primary');
    button.classList.add('btn-primary');
}

function toggleShowPic(src = null){
    let picCont = document.getElementById('pic-show-cont');
    if(picCont.style.display == 'none'){
        picCont.querySelector('img').src = src;
        picCont.style.display = 'flex';
    }
    else {
        picCont.style.display = 'none';
        // document.getElementById('report-desc').value = '';
    }
}



// TODO Check use and remove.
function toggleReportForm(){
    let formCont = document.getElementById('report-form-cont');
    if(formCont.style.display == 'none'){
        formCont.style.display = 'flex';
    }
    else {
        formCont.style.display = 'none';
        document.querySelector('#report-form-cont>.report-form').reset();
    }
}
function togglePicForm(){
    let formCont = document.getElementById('pic-form-cont');
    if(formCont.style.display == 'none'){
        formCont.style.display = 'flex';
    }
    else {
        formCont.style.display = 'none';
        document.getElementById('form-file').value = '';
    }
}

