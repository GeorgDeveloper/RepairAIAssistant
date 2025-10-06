function searchManuals() {
    const query = document.getElementById('searchInput').value.trim();
    
    if (!query) return;
    
    fetch(`/manuals/search?query=${encodeURIComponent(query)}`)
    .then(response => response.text())
    .then(data => {
        document.getElementById('searchResults').innerHTML = `<p>${data}</p>`;
    })
    .catch(error => {
        document.getElementById('searchResults').innerHTML = `<p>Ошибка поиска: ${error.message}</p>`;
    });
}