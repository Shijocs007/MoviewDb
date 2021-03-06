package com.moviedb.movieapp.repository

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.moviedb.movieapp.paging.MovieDataSource
import com.moviedb.movieapp.paging.MovieDataSourceFactory
import com.moviedb.movieapp.paging.POST_PER_PAGE
import com.moviedb.movieapp.models.Movie
import com.moviedb.movieapp.network.MovieApi
import com.moviedb.movieapp.network.NetworkState
import com.moviedb.movieapp.network.SafeApiRequest
import com.moviedb.movieapp.network.model.MovieResult
import com.moviedb.movieapp.room.MovieDatabase
import com.moviedb.movieapp.utils.Utils.KEY_SORT

class MovieRepository
constructor(
    private val api : MovieApi,
    private val db : MovieDatabase,
    private val preferences: SharedPreferences
) : SafeApiRequest() {

    lateinit var moviePagedList: LiveData<PagedList<Movie>>
    lateinit var moviesDataSourceFactory: MovieDataSourceFactory

    suspend fun getMoviesFromCloud(page : Int) : MovieResult{
        val sort : String = preferences.getString(KEY_SORT, "popularity.desc")!!
        return apiRequest { api.getMovies(page, sort) }
    }

    fun fetchMovieList() : LiveData<PagedList<Movie>> {
        moviesDataSourceFactory =
            MovieDataSourceFactory(this)

        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(POST_PER_PAGE)
            .build()
        moviePagedList = LivePagedListBuilder(moviesDataSourceFactory, config).build()
        return moviePagedList
    }

    fun getNetworkState(): LiveData<NetworkState> {
        return Transformations.switchMap<MovieDataSource, NetworkState>(
            moviesDataSourceFactory.moviesLiveDataSource, MovieDataSource::networkState)
    }

    suspend fun getMoviesFromDb(offset : Int) = db.movieDao().getMovies(POST_PER_PAGE,offset)

    suspend fun saveMoviesToDB(movies: List<Movie>) = db.movieDao().upsertAll(movies)
}